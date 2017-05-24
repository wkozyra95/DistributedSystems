package sr.bookshop

case class FindQuery(name: String)
case class OrderQuery(name: String)
case class StreamContentQuery(name: String)
case class FindResponse(book: Array[String])
case class OrderResponse(book: Boolean)
