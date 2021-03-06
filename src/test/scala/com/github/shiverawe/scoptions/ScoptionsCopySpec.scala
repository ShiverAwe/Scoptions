package com.github.shiverawe.scoptions

import org.scalatest.FlatSpec

/**
  * Demo test.
  * Scoptions class has `copyFrom` method which allows to copy all references
  * from one instance to another.
  */
class ScoptionsCopySpec extends FlatSpec {

  case class OuterScoptions() extends Scoptions {
    val inner = InnerScoptions(Wiring(target = this, name = "inner"))
  }

  case class InnerScoptions(override val parent: Wiring) extends Scoptions(parent) {
    val property = PropertyS("inner_property", "default_value")
  }

  "Scoptions" should "copy inner values from another scoptions" in {
    val original = new OuterScoptions()
    val another = new OuterScoptions()

    original.inner.property.set("new_value")

    another.copyFrom(original)

    assert(another.inner.property.get() == "new_value")
  }
}
