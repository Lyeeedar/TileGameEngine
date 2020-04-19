package com.lyeeedar.Components

fun Entity.hasComponent(componentType: ComponentType) = this.signature.contains(componentType)