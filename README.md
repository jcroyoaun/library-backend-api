# spring-boot-api-demo

This is study case on how to approach the container creation and architecture for a Spring Boot app as a DevOps engineer.

To simply test the app without re-creating the steps, just clone this directory on your machine, navigate to the repo directory that resulted in cloning and type :

```
docker-compose up --build
```

And the application + Database should be starting and working without any further configurations.


## The approach I suggest 
If you're onboarding a new project where you want to deploy an application to the Cloud, and the code and schemas have been defined, I suggest you do the following :

### Step 1 (covered in this tutorial) - Start from local environment 
* Make sure the application works in a local environment using some good practices for containers (ex. mounting volumes and using entrypoint for databases to load initial schema).
* Start with a developer friendly container orchestrator like docker-compose. It's easy to use and it will save you some headaches.
* Make sure you can stop, start the containers, delete them and re-create them and the information persists as needed.

#### Local Environment Steup
To keep developing "Step 1" on local environment, I would suggest you go about creating a DB container that matches the technology the developers used. Keep in mind that you might need to modify and make changes to ```src/main/resources/application.properties``` file as that's the file that contains DB connection strings for Spring applications. We don't want our developers to worry about the infrastructure, that's our job, so we will modify and test accordingly.

* Determine DB type
By looking at ```src/main/resources/application.properties``` you'll be able to find what type of database the application is using. For this example, we can tell that this is a MariaDB database, for example it might look something like this :
```
spring.datasource.url=jdbc:mariadb://${DB_HOST}:3306/booksdb
```

* Once you've determined the DB type, let's go ahead and run a database container and load the initial data. 
To decide which docker image to use just go to https://hub.docker.com/_/mariadb and pick whichever you want or suits your specific architecture needs. For this tutorial I'll just use plain "mariadb" database.

It's worth noting that most Database systems will require a few environment variables to be passed on runtime, therefore, If I use something very simple like this:
```
docker run mariadb
```
I'll get the following error message :
```
2022-11-15 05:21:40+00:00 [Note] [Entrypoint]: Entrypoint script for MariaDB Server 1:10.9.4+maria~ubu2204 started.
2022-11-15 05:21:41+00:00 [Note] [Entrypoint]: Switching to dedicated user 'mysql'
2022-11-15 05:21:41+00:00 [Note] [Entrypoint]: Entrypoint script for MariaDB Server 1:10.9.4+maria~ubu2204 started.
2022-11-15 05:21:41+00:00 [ERROR] [Entrypoint]: Database is uninitialized and password option is not specified
	You need to specify one of MARIADB_ROOT_PASSWORD, MARIADB_ROOT_PASSWORD_HASH, MARIADB_ALLOW_EMPTY_ROOT_PASSWORD and MARIADB_RANDOM_ROOT_PASSWORD
```

So moving ahead, this is the command I'll be using to run our DB container the first time :
```
docker run \
  -e MARIADB_PASSWORD=root \
  -e MARIADB_ROOT_PASSWORD=root \
  --name mydb -p 3306:3306 \
  -v $(pwd)/data:/var/lib/mysql \
  mariadb
```

Dissecting the command a bit : 
a) See that we're passing -e (environment variables) with the MARIADB_PASSWORD and MARIADB_ROOT_PASSWORD values to allow the container to start.
b) We're passing a local directory as a volume mounted FROM our local... path before the column ":", and the directory that mariadb uses to write data as more info gets added to the DB.
```
-v $(pwd)/data:/var/lib/mysql
```

This will essentially make the container write everything that's happening in the RDBMS to our hard drive so that everytime the container starts, it will persist the data (or we won't lose our newly created entries). This is called mounting a volume and is a BEST PRACTICE when working with database containers. For local development, we might use a local directory as a "mounted volume". In the cloud, we use a service like AWS S3, or AWS EBS. In this example we are using the data directory found in this repo as a local mount. You'll be able to see how that directory starts being populated with files as soon as we start the container and start writing tables and info in the DB schemas.

c) Now that we have the database started the first time, we want to add values to them or create the tables. First we are going to explore a solution that allows us to create values directly from a schema.sql file.

#### Option 1) Adding values FROM a schema file.
Chances are developers have access to the data they used during the development phase. If they do, just ask them to run this command and send you the resulting .sql

In this example, we're assuming they now the name of the database where the data is created and the database is called booksdb :
```
mysqldump -h localhost -u root -p --databases booksdb  > schema.sql
```

#### Option 2) Load the data manually 
If you were able to follow through with Option 1, then move to "Creating a Dockerfile to load data" section, otherwise, read on :

If you need to load the data manually, you can do so by typing a few commands. For this example, I'm just going to pretend we're creating the booksdb database along with the book table and a sample row of data. I'll explain everything from accessing the container via ssh all the way to the commands necessary to load the data the first time and create a dump directly from the container running right into your local :

let's make sure the container mydb is running :
```
docker ps
```

if it's not running, do 
```
docker rm mydb 
```
```
docker run \
  -e MARIADB_PASSWORD=root \
  -e MARIADB_ROOT_PASSWORD=root \
  --name mydb -p 3306:3306 \
  -v $(pwd)/data:/var/lib/mysql \
  mariadb
```

if it's already running, then do :
```
docker exec -it mydb /bin/bash
```

Once inside the container, run :
```
mysql -u root -p
```
Once it asks you for the password, type "root" as the password.

To create the database as intended for this example, do 
```
create database booksdb;
```
```
use booksdb;
```
```
CREATE TABLE BOOK(
    ISBN VARCHAR(13) PRIMARY KEY, 
    NAME VARCHAR(255),
    AUTHOR VARCHAR(255)
);
INSERT INTO BOOK (ISBN, NAME, AUTHOR)
VALUES ('9873161484100', 'Test Book', 'Test Author');
```

Once we have the data added, exit the container (typing exit until you're back to your localhost shell)
And then run the following command to dump the database schema.sql from the container into our local computer :
```
docker exec -it mydb mysqldump -h localhost -u root -p --databases booksdb > schema.sql
```

We have a schema.sql with the syntax on how to create the exact database the developers have, now what?

It turns out that Docker DB images have a directory called "/docker-entrypoint-initdb.d/" that is used as an entry point to load the initial DB statements that we want our DBs to run into, so we can take advantage of it by copying the file directly into it. For that, I recommend creating a Dockerfile.

#### Creating a Dockerfile to load data 
let's create a directory called mariadb and copy the schema.sql there
```
mkdir -p mariadb
cp schema.sql mariadb/.
```

Inside this directory, we will create a Dockerfile that copies that schema.sql inside our mariadb container :
```
vi mariadb/Dockerfile
```
This is what goes into the Dockerfile
```
FROM mariadb
COPY ./schema.sql docker-entrypoint-initdb.d/
```

Now we build this image to test it out
``` 
docker build -t mydb -f mariadb/Dockerfile mariadb/.
```
And we run it again :

```
docker stop mydb

docker rm mydb

docker run \                                        
  -e MARIADB_PASSWORD=root \
  -e MARIADB_ROOT_PASSWORD=root \
  --name mydb -p 3306:3306 \
  -v $(pwd)/data:/var/lib/mysql \
  mariadb
```

Now that we have a database loaded with data, we can proceed to run our Spring Boot application

Note that Spring Boot apps run in 2 steps, 
1. Build (we have to build everytime we make changes to .properties or to the code base)
```
mvn package
```

2. run
```
java -jar target/java-docker-demo-0.0.1-SNAPSHOT.jar
```

If everything went well with the previous steps, we will see our application running on localhost:8080.

```
localhost:8080/api/v1/books
```
Should return the values we added to the database earlier.

Now, let's convert this Spring Boot app to containers as well, for this we will create a 2 stage Dockerfile like this one :

```
FROM maven:3.8.6-openjdk-18 as builder

WORKDIR /usr/app/
COPY . /usr/app
RUN mvn package -Dmaven.test.skip
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app.jar

FROM openjdk:18
WORKDIR /usr/app
COPY --from=builder /app.jar /usr/app
EXPOSE 8080

CMD ["java","-jar","app.jar"]
```

Note that we're using a ```maven:3.8.6-openjdk-18``` for the build phase, and an ```openjdk:18``` for the run phase. Chances are your Java version is different, so make sure you check with the developers which versions they used. This is how I checked on my local environment what did I use for coding, and therefore I made the decision of which images from Docker to containerize my application upon

```
% mvn -version                                        
Apache Maven 3.8.6 (84538c9988a25aec085021c365c560670ad80f63)
Maven home: /usr/local/bin/apache-maven-3.8.6
Java version: 18, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/jdk-18.jdk/Contents/Home
Default locale: en_MX, platform encoding: UTF-8
OS name: "mac os x", version: "12.3.1", arch: "aarch64", family: "mac"


% java -version
java version "18" 2022-03-22
Java(TM) SE Runtime Environment (build 18+36-2087)
Java HotSpot(TM) 64-Bit Server VM (build 18+36-2087, mixed mode, sharing)
```

Once you have the Dockerfile ready, we want to build the container. To do that, simply type
```
docker build -t spring-boot .
```

then 
```
docker run -p 8080:8080 --link mydb spring-boot
```
NOTE : --link is a deprecated docker option, we normally want to go by creating networks but We're just testing the container works so far.


At this point, everything should work, now please see how I built everything into Docker Compose by looking at the docker-compose.yaml.

To run the docker-compose version, just type

```
docker-compose up --build
```


Thank you.


### Step 2 Create CI/CD pipeline
### Step 3 TBD...
