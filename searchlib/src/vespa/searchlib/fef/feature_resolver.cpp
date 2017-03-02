// Copyright 2017 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "feature_resolver.h"

namespace search {
namespace fef {

FeatureResolver::FeatureResolver(size_t size_hint)
    : _names(),
      _features(),
      _is_object()
{
    _names.reserve(size_hint);
    _features.reserve(size_hint);
    _is_object.reserve(size_hint);
}

FeatureResolver::~FeatureResolver() { }

} // namespace fef
} // namespace search
