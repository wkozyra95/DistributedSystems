package sr.bookshop


case class BookDescription(name: String, price: Int) {
  def print(): Unit = {
    Console.print("Book: " + name + ", Price: " + price.toString)
  }
}

case class BookContent(content: String) {
  def print(): Unit = {
    Console.print(content)
  }
}