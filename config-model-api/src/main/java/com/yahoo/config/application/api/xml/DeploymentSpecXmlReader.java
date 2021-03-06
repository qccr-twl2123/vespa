package com.yahoo.config.application.api.xml;

import com.yahoo.config.application.api.DeploymentSpec;
import com.yahoo.config.application.api.DeploymentSpec.Step;
import com.yahoo.config.application.api.DeploymentSpec.Delay;
import com.yahoo.config.application.api.DeploymentSpec.DeclaredZone;
import com.yahoo.config.application.api.DeploymentSpec.ParallelZones;
import com.yahoo.config.application.api.DeploymentSpec.ChangeBlocker;
import com.yahoo.config.application.api.TimeWindow;
import com.yahoo.config.provision.Environment;
import com.yahoo.config.provision.RegionName;
import com.yahoo.io.IOUtils;
import com.yahoo.text.XML;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author bratseth
 */
public class DeploymentSpecXmlReader {

    private static final String testTag = "test";
    private static final String stagingTag = "staging";
    private static final String blockChangeTag = "block-change";
    private static final String prodTag = "prod";
    
    public DeploymentSpec read(Reader reader) {
        try {
            return read(IOUtils.readAll(reader));
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Could not read deployment spec", e);
        }
    }

    /** Reads a deployment spec from XML */
    public DeploymentSpec read(String xmlForm) {
        List<Step> steps = new ArrayList<>();
        Optional<String> globalServiceId = Optional.empty();
        Element root = XML.getDocument(xmlForm).getDocumentElement();
        validateTagOrder(root);
        for (Element environmentTag : XML.getChildren(root)) {
            if ( ! isEnvironmentName(environmentTag.getTagName())) continue;

            Environment environment = Environment.from(environmentTag.getTagName());

            if (environment == Environment.prod) {
                for (Element stepTag : XML.getChildren(environmentTag)) {
                    if (stepTag.getTagName().equals("delay")) {
                        steps.add(new Delay(Duration.ofSeconds(longAttribute("hours", stepTag) * 60 * 60 +
                                                               longAttribute("minutes", stepTag) * 60 +
                                                               longAttribute("seconds", stepTag))));
                    } else if (stepTag.getTagName().equals("parallel")) {
                        List<DeclaredZone> zones = new ArrayList<>();
                        for (Element regionTag : XML.getChildren(stepTag)) {
                            zones.add(readDeclaredZone(environment, regionTag));
                        }
                        steps.add(new ParallelZones(zones));
                    } else { // a region: deploy step
                        steps.add(readDeclaredZone(environment, stepTag));
                    }
                }
            } else {
                steps.add(new DeclaredZone(environment));
            }

            if (environment == Environment.prod)
                globalServiceId = readGlobalServiceId(environmentTag);
            else if (readGlobalServiceId(environmentTag).isPresent())
                throw new IllegalArgumentException("Attribute 'global-service-id' is only valid on 'prod' tag.");
        }
        return new DeploymentSpec(globalServiceId, readUpgradePolicy(root), readChangeBlockers(root), steps, xmlForm);
    }

    /** Imposes some constraints on tag order which are not expressible in the schema */
    private void validateTagOrder(Element root) {
        List<String> tags = XML.getChildren(root).stream().map(Element::getTagName).collect(Collectors.toList());
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equals(blockChangeTag)) {
                String constraint = "<block-change> must be placed after <test> and <staging> and before <prod>";
                if (containsAfter(i, testTag, tags)) throw new IllegalArgumentException(constraint);
                if (containsAfter(i, stagingTag, tags)) throw new IllegalArgumentException(constraint);
                if (containsBefore(i, prodTag, tags)) throw new IllegalArgumentException(constraint);
            }
        }
    }
    
    private boolean containsAfter(int i, String item, List<String> items) {
        return items.subList(i+1, items.size()).contains(item);
    }

    private boolean containsBefore(int i, String item, List<String> items) {
        return items.subList(0, i).contains(item);
    }

    /** Returns the given attribute as an integer, or 0 if it is not present */
    private long longAttribute(String attributeName, Element tag) {
        String value = tag.getAttribute(attributeName);
        if (value == null || value.isEmpty()) return 0;
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected an integer for attribute '" + attributeName +
                                               "' but got '" + value + "'");
        }
    }

    private boolean isEnvironmentName(String tagName) {
        return tagName.equals(testTag) || tagName.equals(stagingTag) || tagName.equals(prodTag);
    }

    private DeclaredZone readDeclaredZone(Environment environment, Element regionTag) {
        return new DeclaredZone(environment, Optional.of(RegionName.from(XML.getValue(regionTag).trim())),
                                readActive(regionTag));
    }

    private Optional<String> readGlobalServiceId(Element environmentTag) {
        String globalServiceId = environmentTag.getAttribute("global-service-id");
        if (globalServiceId == null || globalServiceId.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(globalServiceId);
        }
    }

    private List<DeploymentSpec.ChangeBlocker> readChangeBlockers(Element root) {
        List<DeploymentSpec.ChangeBlocker> changeBlockers = new ArrayList<>();
        for (Element tag : XML.getChildren(root)) {
            // TODO: Remove block-upgrade on Vespa 7
            if ( ! blockChangeTag.equals(tag.getTagName()) && !"block-upgrade".equals(tag.getTagName())) continue;

            boolean blockVersions = trueOrMissing(tag.getAttribute("version"));
            boolean blockRevisions = trueOrMissing(tag.getAttribute("revision"))
                                     && !tag.getTagName().equals("block-upgrade"); //  TODO: Remove condition on Vespa 7

            String daySpec = tag.getAttribute("days");
            String hourSpec = tag.getAttribute("hours");
            String zoneSpec = tag.getAttribute("time-zone");
            if (zoneSpec.isEmpty()) { // Default to UTC time zone
                zoneSpec = "UTC";
            }
            changeBlockers.add(new DeploymentSpec.ChangeBlocker(blockRevisions, blockVersions,
                                                                TimeWindow.from(daySpec, hourSpec, zoneSpec)));
        }
        return Collections.unmodifiableList(changeBlockers);
    }

    /** Returns true if the given value is "true", or if it is missing */
    private boolean trueOrMissing(String value) {
        return value == null || value.isEmpty() || value.equals("true");
    }

    private DeploymentSpec.UpgradePolicy readUpgradePolicy(Element root) {
        Element upgradeElement = XML.getChild(root, "upgrade");
        if (upgradeElement == null) return DeploymentSpec.UpgradePolicy.defaultPolicy;

        String policy = upgradeElement.getAttribute("policy");
        switch (policy) {
            case "canary" : return DeploymentSpec.UpgradePolicy.canary;
            case "default" : return DeploymentSpec.UpgradePolicy.defaultPolicy;
            case "conservative" : return DeploymentSpec.UpgradePolicy.conservative;
            default : throw new IllegalArgumentException("Illegal upgrade policy '" + policy + "': " +
                                                         "Must be one of " + Arrays.toString(DeploymentSpec.UpgradePolicy.values()));
        }
    }

    private boolean readActive(Element regionTag) {
        String activeValue = regionTag.getAttribute("active");
        if ("true".equals(activeValue)) return true;
        if ("false".equals(activeValue)) return false;
        throw new IllegalArgumentException("Region tags must have an 'active' attribute set to 'true' or 'false' " +
                                           "to control whether the region should receive production traffic");
    }

}
