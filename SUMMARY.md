# Summary

## Comments

The task is easy to understand. The only thing is in the `assignment.md` the API responses are shown to return
plain JSON that does not comply to internal `ResponseEntity` class structure. Looking at the `StatusController`,
to comply to the service API structure, I anyway used internal `ResponseEntity` class for 
responses (except file content API).

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
You need `mvn` to build the project

```shell
mvn clean package
```

Produces fat-jar at `target/ROOT.jar`

### Run the jar file

1. Build the project with `mvn`
2. set variables in `variables.env` file
3. by specifying the correct path to `variables.env` run in shell:

```shell
./do.sh (runs with mvn)
or
export $(cat variables.env | xargs) && java -jar target/ROOT.jar
```

### Running in JetBrains IDEA

Import pom file and run the Application run configuration.

### Running tests

There exists basic functional tests using MongoDB TestContainer. To run the tests do:

```shell
mvn clean test
```

Test creates `test_uploades` folder in the current directory to store uploaded test files.

After test completes, `mongodb` client throws `Prematurely reached end of stream` exception. It is not app error.
You can ignore it. I didnt have time to dig into it deeper, the driver complains to shutdown after test.

## Implementation notes

1. Initially thought to create separate executorService for IO operations. But read that Kotlin already
   provides IO dispatcher for such case. I used them for store and get file content APIs.
2. Delete file API is idempotent. It returns always OK even if file not found. But logs the warning messages
   accordingly. File deletion is executed in the background without blocking using IO dispatcher.
3. Get file content API returns 404 if either metadata or file itself was not found. Error message with
   details is logged.
4. Get file metas API returns empty for invalid or non existing tokens.
5. For each uploaded file, the chain of subdirectories are created to store the file. Each subdirectory is the name of
   next letter in the UUID filename (exlcuding "-"). For example, for UUID filename `a-b-c`, such parent folders are
   created: `/a/b/c/` This is due to the fact, that storing a lot of files in single folder on filesystem
   can beat the system performance. Even tough we could create since there would not be filename collisions.
6. For managing file I created abstract `FileManager` interface. Since task requirement is store file on filesystem,
   I implemented `FileSystemFileManager`. However, if S3 or other NAS like storage are needed, we could implement them
   by using `FileManager` and swap with `Qualifier`.
