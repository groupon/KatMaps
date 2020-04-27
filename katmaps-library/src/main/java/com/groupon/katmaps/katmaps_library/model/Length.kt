/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package com.groupon.katmaps.katmaps_library.model

sealed class LengthUnit(val ratio: Double) {
    object Meter : LengthUnit(1.0)
    object Kilometer : LengthUnit(1000.0)
    object Foot : LengthUnit(0.3048)
    object Mile : LengthUnit(1609.34)
}

class Length private constructor(internal val baseValue: Double) : Comparable<Length> {
    companion object {
        @JvmStatic
        fun fromMeters(m: Double) = Length(m, LengthUnit.Meter)

        @JvmStatic
        fun fromKilometers(km: Double) = Length(km, LengthUnit.Kilometer)

        @JvmStatic
        fun fromFeet(ft: Double) = Length(ft, LengthUnit.Foot)

        @JvmStatic
        fun fromMiles(mi: Double) = Length(mi, LengthUnit.Mile)
    }

    constructor(value: Double, unit: LengthUnit) : this(value * unit.ratio)

    val meters: Double
        get() = to(LengthUnit.Meter)

    val kilometers: Double
        get() = to(LengthUnit.Kilometer)

    val feet: Double
        get() = to(LengthUnit.Foot)

    val miles: Double
        get() = to(LengthUnit.Mile)

    infix fun to(unit: LengthUnit): Double = baseValue / unit.ratio

    operator fun plus(increment: Length) = Length(baseValue + increment.baseValue)
    operator fun minus(decrement: Length) = Length(baseValue + decrement.baseValue)
    operator fun times(factor: Number) = Length(baseValue * factor.toDouble())
    operator fun div(divisor: Number) = Length(baseValue / divisor.toDouble())
    override fun equals(other: Any?) = baseValue == (other as? Length)?.baseValue
    override fun hashCode(): Int = baseValue.hashCode()
    override fun compareTo(other: Length) = baseValue.compareTo(other.baseValue)
}

val Number.meters: Length get() = Length(this.toDouble(), LengthUnit.Meter)
val Number.kilometers: Length get() = Length(this.toDouble(), LengthUnit.Kilometer)
val Number.feet: Length get() = Length(this.toDouble(), LengthUnit.Foot)
val Number.miles: Length get() = Length(this.toDouble(), LengthUnit.Mile)

fun min(a: Length, b: Length): Length = if (a.baseValue < b.baseValue) a else b
fun max(a: Length, b: Length): Length = if (a.baseValue > b.baseValue) a else b
