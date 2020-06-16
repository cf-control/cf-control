# cf-control [![Coverage Status](https://coveralls.io/repos/github/cf-control/cf-control/badge.svg?branch=master)](https://coveralls.io/github/cf-control/cf-control?branch=master)

Declarative command line tool for Pivotal Cloud Foundry.


## Prerequisites

* Java JRE (Version 8 or newer).
* Gradle


## Environment Setup

Follow the instructions below to setup your environment so that
you can run the __cf-control__ application.

* Download the source code from GitHub.

  ``` git clone https://github.com/cf-control/cf-control.git```

* Change the directory and build the project.

  ```cd cf-control/cloud.foundry.cli && ./gradlew build```

  This command will generate a JAR output __cf-control.jar__
  in the **_cf-control/cloud.foundry.cli/build/libs_** folder.

* Define environment variables (Optional).

    To reduce the amount of parameters on the command line, you can define environment variables for your credentials.

  * ```CF_CONTROL_USER``` describes your account's e-mail address or username.
  * ```CF_CONTROL_PASSWORD``` describes the password of your cf account.


## How to execute a command?

Change the current directory to the JAR output folder `cf-control/cloud.foundry.cli/build/libs`.

Then you can run the `cf-control.jar` by using the command line tool like:

```
java -jar cf-control.jar [COMMAND] [SUBCOMMAND] [PARAMS]
```


## Logging

The application logs its activities using multiple loglevels. At the moment, the default level is `INFO`. Verbosity can be increased to *verbose* or even *debug* logging by setting the `VERBOSE` respectively `DEBUG` environment variables to any value.

The loglevel is always configured to the most verbose value the user specified. For instance, if both *verbose* and *debug* logging are enabled, the application will use the *debug* level.


## Available commands

#### [COMMAND]

```
  create          Create a new app, service instance or add a new space developer.
  get             Get the current configuration of your cf instance.
  diff            Print the differences between the given yaml file and the configuration of your cf instance.
  apply           Apply the configuration from a given yaml file to your cf instance.
  update          Update/Remove apps, service instances or space developers.
```


##### create [SUBCOMMAND]

```
  service          Create services in the target space.
  space-developer  Assign users as space developers.
  application      Create applications in the target space.
```


##### get [SUBCOMMAND]

```
  services          List all services in the target space.
  space-developers  List all space developers in the target space.
  applications      List all applications in the target space.
  all               Show all information in the target space.
```


##### diff [SUBCOMMAND]

```
  applications      Print the differences between the apps given in the yaml file and
                     the configuration of the apps of your cf instance.
  services          Print the differences between the services given in the yaml file and
                     the configuration of the services of your cf instance.
```

##### update [SUBCOMMAND]

```
  remove-service          Remove service instances in the target space.
  update-service          Update service instances in the target space.
  remove-space-developer  Remove space developers in the target space.
  update-application      Update applications in the target space.
  remove-application      Remove applications in the target space.
```


##### [PARAMS]

```
  -a, --api=<apiHost>                    Your CF instance's API endpoint URL.
  -o, --organization=<organization>      Your CF organization's name.
  -p, --password=<password>              Your password of your cf account.
  -s, --space=<space>                    Your CF space name.
  -u, --user=<userName>                  Your account's e-mail address or username.
  -f, --force                            Force deletion without confirmation.
  -y, --yaml=<yamlFile>                  The path to the yaml file. (Not needed for the get commands)
```


  for example, you can run the command: \
  ```java -jar cf-control.jar get services -a api.run.pivotal.io -o cloud.foundry.cli -s development -u mustermann@test.com -p somePassword;)```


### Convenience features

You are able to include contents of other YAML files into the one that you pass as an argument to the tool. To do so, you have to use the so called **$ref-syntax**. A description on how to use it can be found [here](https://swagger.io/docs/specification/using-ref/).
* The tool supports **Remote References** and **URL References**.
* **Local References** are not natively supported, but you can achieve the same effect by using **Anchors and Alisases** as they are stated in the YAML specification.


### Demo Files

In this [folder](cloud.foundry.cli/src/test/resources/demo/) we provide example YAML files that may help in
understanding how to use our tool. What can I use the files for?

* [appJava.yml](cloud.foundry.cli/src/test/resources/demo/appJava.yml): Reference for create/update/remove of an 
application.
* [appPython.yml](cloud.foundry.cli/src/test/resources/demo/appPython.yml): Reference for create/update/remove of an
application.
* [getAll.yml](cloud.foundry.cli/src/test/resources/demo/appJava.yml): Example output of the **get all** command. 
In this exmaple you can see how you have to structure the yml Files in general.
* [refDemo.yml](cloud.foundry.cli/src/test/resources/demo/refDemo.yml) & 
:[referred.yml](cloud.foundry.cli/src/test/resources/demo/referred.yml) Example how you can use the ref syntax in the 
context of creating services (command: create services)
* [services.yml](cloud.foundry.cli/src/test/resources/demo/services.yml): Reference for create/remove of 
services.
* [spaceDeveloper.yml](cloud.foundry.cli/src/test/resources/demo/spaceDeveloper.yml):  Reference for create/remove of 
space developers.

__Happy coding!__
