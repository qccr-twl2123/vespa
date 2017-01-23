// Copyright 2017 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.document.datatypes;

import com.yahoo.document.DataType;
import com.yahoo.document.DocumentId;
import com.yahoo.document.Field;
import com.yahoo.document.ReferenceDataType;
import com.yahoo.document.serialization.FieldReader;
import com.yahoo.document.serialization.FieldWriter;
import com.yahoo.document.serialization.XmlStream;

import java.util.Objects;
import java.util.Optional;

/**
 * A reference field value allows search queries to access fields in other document instances
 * as if they were fields natively stored within the searched document. This allows modelling
 * one-to-many relations such as a parent document with many children containing references
 * back to the parent.
 *
 * Each <code>ReferenceFieldValue</code> may contain a single document ID which specifies the
 * instance the field should refer to. This document ID must have a type matching that of the
 * reference data type of the field itself.
 *
 * Note that references are not polymorphic. This means that if you have a document type
 * "foo" inheriting "bar", you cannot have a <code>reference&lt;bar&gt;</code> field containing
 * a document ID for a "foo" document.
 *
 * @author vekterli
 * @since 6.65
 */
public class ReferenceFieldValue extends FieldValue {

    private final ReferenceDataType referenceType;
    private Optional<DocumentId> documentId;

    /**
     * Creates an empty reference of the provided reference type.
     * @param referenceType reference target type
     */
    public ReferenceFieldValue(ReferenceDataType referenceType) {
        this.referenceType = referenceType;
        this.documentId = Optional.empty();
    }

    /**
     * Creates a reference pointing to a particular document instance in the document
     * type referenced by <code>referenceType</code>.
     * @param referenceType reference target type
     * @param documentId document ID of the same document type as that given by <code>referenceType</code>
     * @throws IllegalArgumentException if documentId is not of the expected document type
     */
    public ReferenceFieldValue(ReferenceDataType referenceType, DocumentId documentId) {
        requireIdOfMatchingType(referenceType, documentId);
        this.referenceType = referenceType;
        this.documentId = Optional.of(documentId);
    }

    public static ReferenceFieldValue createEmptyWithType(ReferenceDataType referenceType) {
        return new ReferenceFieldValue(referenceType);
    }

    private static void requireIdOfMatchingType(ReferenceDataType referenceType, DocumentId id) {
        final String expectedTypeName = referenceType.getTargetType().getName();
        if (!id.getDocType().equals(expectedTypeName)) {
            throw new IllegalArgumentException(String.format(
                    "Can't assign document ID '%s' (of type '%s') to reference of document type '%s'",
                    id, id.getDocType(), expectedTypeName));
        }
    }

    @Override
    public DataType getDataType() {
        return referenceType;
    }

    public Optional<DocumentId> getDocumentId() {
        return documentId;
    }

    public void setDocumentId(DocumentId documentId) {
        this.documentId = Optional.of(documentId);
    }

    @Override
    public void printXml(XmlStream xml) {
        // TODO do we need/want this?
    }

    @Override
    public void clear() {
        this.documentId = Optional.empty();
    }

    @Override
    public void assign(Object o) {
        if (o == null) {
            clear();
        } else if (o instanceof DocumentId) {
            this.documentId = Optional.of((DocumentId)o);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Can't assign value of type '%s' to field of type '%s'. Expected value of type '%s'",
                    o.getClass().getName(), getClass().getName(), DocumentId.class.getName()));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ReferenceFieldValue that = (ReferenceFieldValue) o;
        return Objects.equals(referenceType, that.referenceType) &&
                Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referenceType, documentId);
    }

    @Override
    public void serialize(Field field, FieldWriter writer) {
        writer.write(field, this);
    }

    @Override
    public void deserialize(Field field, FieldReader reader) {
        reader.read(field, this);
    }
}
