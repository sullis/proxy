package controllers

case class Service(name: String, host: String)

object Services {

  val Token = Service("token", "http://localhost:6151")

}
