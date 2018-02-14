package com.github.shiverawe.scoptions

import scala.collection.mutable

/**
  * Class allows
  *  - bind properties to be managed by Scoptions
  *  - parse and apply command line arguments to properties
  */
abstract class Scoptions(val parent: Wiring = Scoptions.WIRING_UNDEFINED) extends PropertyPack with ScoptionsPack {
  val outerScope: ScoptionsPack = parent.outerScope
  val name: String = parent.name
  /**
    * Properties defined in inherited classes use this value to register
    */
  implicit val target: Option[PropertyPack] = Some(this)

  def applyArgument(argument: String): Boolean = {
    applyArgumentThere(argument) || applyArgumentInside(argument)
  }

  def copyFrom(source: Scoptions): this.type = {
    copyFromThere(source).copyFromInside(source)
  }

  def copyFromThere(source: Scoptions): this.type = {
    source.registeredProperties.keys.foreach(key => {
      registeredProperties.get(key).get.setContent(
        source.registeredProperties(key).getContent())
    })
    this
  }

  def copyFromInside(source: Scoptions): this.type = {
    this.registeredSubScoptions.keys.foreach(key => {
      this.registeredSubScoptions(key).copyFromThere(
        source.registeredSubScoptions(key))
    })
    this
  }

  def disassembly: Seq[String] = {
    disassemblyThere() ++ disassemblyInside()
  }.distinct

  override def toString: String = {
    var string = ""
    registeredProperties.foreach(p =>
      string += s"${p._1}=${p._2} ")
    string
  }

  //--------------------------------CONSTRUCTOR-------------------------
  {
    if (outerScope != Scoptions.ROOT_UNDEFINED &&
      outerScope != Scoptions.ROOT_DEFINED)
      outerScope.registerSubScoptions(this)
  }
}

object Scoptions {
  val ROOT_UNDEFINED: Scoptions = new Scoptions(Wiring(null, name = "GLOBAL_SCOPTIONS_ROOT_UNDEFINED")) {}
  val ROOT_DEFINED: Scoptions = new Scoptions(Wiring(null, name = "GLOBAL_SCOPTIONS_ROOT")) {}
  val WIRING_UNDEFINED: Wiring = Wiring(ROOT_UNDEFINED)
  val WIRING_DEFINED  : Wiring = Wiring(ROOT_DEFINED)
}

trait ScoptionsPack {
  val outerScope: ScoptionsPack

  var registeredSubScoptions = Map[String, Scoptions]()

  def registerSubScoptions(scoptions: Scoptions): Unit = {
    if (registeredSubScoptions contains scoptions.name)
      throw new IllegalArgumentException(s"That scoptions are already [${scoptions.name}]")
    registeredSubScoptions = registeredSubScoptions + (scoptions.name -> scoptions)
  }

  def applyArgumentInside(argument: String): Boolean = {
    registeredSubScoptions.values.foldLeft(false)((b: Boolean, inner: Scoptions) =>
      b || inner.applyArgument(argument) // was applied previously OR just now OR inside
    )
  }

  def disassemblyInside(): Seq[String] = {
    registeredSubScoptions.values.foldLeft(Seq[String]()) { (seq: Seq[String], inner: Scoptions) =>
      seq ++ inner.disassembly
    }
  }
}

trait PropertyPack {
  /**
    * Collection of properties, registered as dependent
    */
  protected val registeredProperties = mutable.Map[String, PropertyLike[_]]()

  val name: String

  /**
    * Allows to parse and apply command line arguments to all registered properties
    */
  def applyArgumentThere(argument: String): Boolean = {
    if (argument == "") return true // TODO VShefer 25 jan: Its a hack. "" means no argument. Need to use Option with None
    val kv = parseArgument(argument)
    if (registeredProperties contains kv._1) {
      registeredProperties(kv._1).fromString(kv._2)
      true
    } else false
  }

  def disassemblyThere(): Seq[String] =
    registeredProperties.values.map(_.disassembly).toSeq

  /**
    * Use this method to define which properties should be managed by this class
    *
    * @param properties properties to be managed by this class
    */
  def registerProperties(properties: PropertyLike[_]*) =
    properties.foreach(p => {
      // TODO delete debug output
      //println(s"Registering ${p.key}")
      registeredProperties += (p.key -> p)
    })

  /**
    * Splits a command line argument into key-value pair
    *
    * @param argument command line argument
    * @return (key, value) tuple
    */
  private def parseArgument(argument: String): (String, String) = {
    val delimiter = "="
    val pos = argument.indexOf(delimiter)
    if (pos == 0) throw new IllegalArgumentException(s"$argument does not match pattern `key=value`: Starts with `$delimiter`")
    if (pos < 0) throw new IllegalArgumentException(s"$argument does not match pattern `key=value`: No delimiter `$delimiter` found")
    val key = argument.substring(0, pos)
    val value = argument.substring(pos + delimiter.length)
    (key, value)
  }
}
