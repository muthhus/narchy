class Hello {
    def methodMissing(String name, args) {
        println "Hello ${name}!"
    }
}

def hello = new Hello()
hello.Groovy()
hello.Javascript()
hello.grooscript()