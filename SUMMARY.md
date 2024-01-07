# Summary

## Comments

The task is easy to understand.

## Which part of the assignment took the most time and why?

Implementing in kotlin and understanding how threading/coroutine works, because I am the java developer.
However, it didn't take much time to learn.

## What You learned

Syntax is pretty easy. But learned some basics of Kotlin coroutine (types of Dispatchers) which was important to
handle IO operations.

## Building the application

### Requirements

- maven
- docker
- mongodb
- java 17

### How to build

```shell
mvn clean package
```

Produces fat-jar at `target/ROOT.jar`

### Run the project

1. set variables in `variables.env` file
2. by specifying the correct path to `variables.env` run in shell:

```shell
export $(cat variables_local.env | xargs) && java -jar ROOT.jar
```

### Running in JetBrains IDEA

Import pom file and run the Application run configuration.

### Running tests

There exists basic functional tests using MongoDB TestContainer. To run the tests do:

```shell
mvn clean test
```

After test completes mongodb client throws `Prematurely reached end of stream` exception. It is not app error.
You can ignore it. 

