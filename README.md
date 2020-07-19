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

The application logs its activities using multiple loglevels. At the moment, the default level is `INFO`. Verbosity can be increased to *verbose* or even *debug* logging by setting the `VERBOSE` respectively `DEBUG` environment variables to any value, or by using the `--debug/-d` and `-v/--verbose` CLI options.

The loglevel is always configured to the most verbose value the user specified. For instance, if both *verbose* and *debug* logging are enabled, the application will use the *debug* level.

The application also supports a quiet mode, which can be enabled by setting the environment variable `QUIET` to any value or via the `-q/--quiet` CLI option.

You can have the tool create a machine-readable log file by specifying the `--log-file my.log` parameter. The output format is XML and should be straightforward to read and parse.


## Available commands

#### [COMMAND]

```
  get             Show all information of your cf instance.
  diff            Print the differences between the given yaml file and the configuration of your cf instance.
  apply           Apply the configuration from a given yaml file to your cf instance.
  rename          Rename an app or a service instance.
  dump            Read a configuration file, resolve all $refs and print the result to the console.
                  Helps users to understand how the tool resolves $ref and what the resulting config is
                  it would apply.
```

##### apply [SUBCOMMAND]
```
  space-developers   Apply the differences between the space developers given in the YAML file and
                     in the live system.

  services           Create/remove services that are present in the given yaml file, but not in your cf instance.
  
  applications       Apply the differences between the apps given in the yaml file and
                     the configuration of the apps of your cf instance.
```

##### rename [SUBCOMMAND]
```
  application        Rename an existing app.

  service            Rename an existing service.
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
  -ns --no-auto-start                    Don't start apps when they get deployed (only for the apply command) 
```

##### [HINT - DEFAULT VALUES FOR SOME PARAMS]

To reduce the number of the program parameters `api`, `organization` and `space`, there is a property file [cf_control.properties](cloud.foundry.cli/src/main/resources/cf_control.properties) that defines default values for the <code>get</code> command.
For the <code>diff/apply</code> commands, the paramters can be fetched from the target section of the given YAML file.

So the default values are defined in the cf_control.properties file:

```
  -a=api.run.pivotal.io
  -o=cloud.foundry.cli
  -s=development
```
the defined values in a given YAML file could be:

```
target:
  endpoint=api.run.pivotal.io
  org=cloud.foundry.cli
  space=development
```

This reduces the number of program parameters to be called.
However, if a value other than the default value is required, 
this corresponding parameter can be set with its value in the program call.

*The specification is:*  
Passed value -> overwrites default value  
No value -> default value is used 

  For example, you can run the following commands: 
  
 <code>get</code> command:
 
 `java -jar cf-control.jar get -u mustermann@test.com -p somePassword`
  
 <code>diff</code> and <code>apply</code> commands:
 
 `java -jar cf-control.jar diff -u mustermann@test.com -p somePassword -y pathToYamlFile`
 
 `java -jar cf-control.jar apply -u mustermann@test.com -p somePassword -y pathToYamlFile`
 
 
### YAML Specification Reference

```
apiVersion: 1.0                           # the version of the yaml specification
target:
  endpoint: api.run.pivotal.io            # the host of your cloud foundry instance
  org: cloud.foundry.cli                  # the organization on your cloud foundry instance
  space: test                             # the space on your organization
spec:
  spaceDevelopers:                        # list of space developers on your cloud foundry instance
    - max.mustermann@emailservice.de
    ...
  services:
    sqlService:                           # name of your service instance
      service: mySQL                      # service type
      plan: free-plan                     # plan that your service uses
      tags:                               # list of tags that the service instance should have
        - development
    scalingService:
      ...
  apps:
    springApp:                            # name of the app
      path: ../sources/app                # the path to the the app source (can be a folder or a file)
      meta: SNAPSHOT-1.0                  # provides a way to specify user provided meta info
      manifest:
        buildpack: java_buildpack         # buildpack that should be used when deploying the app
        command: ./gradlew run            # custom command to start the app
        disk: 512                         # disk space per instance in MB
        memory: 512                       # amount of memory per instance in MB
        instances: 4                      # number of instances that should run
        environmentVariables:             # user specified environment variables the app can use
          APP_ENVIRONMENT: TEST
        healthCheckHttpEndpoint: /health  # sets the endpoint when health check type HTTP was used
        healthCheckType: HTTP             # sets the health check type [PORT, HTTP, PROCESS] 
        noRoute: false                    # overrides other routes settings when set to true
        randomRoute: false                # creates a random route (for testing purposes useful)
        routes:                           # list of routes the app should be mapped to
          -development.mycf.io
        services:                         # list of services the app should be bound to
          - sqlService
        stack: cflinuxfs3                 # species which stack to deploy your app to
        timeout: 80                       # number of seconds the cf allocates for starting the app
    pythonApp:
      ...
```

### Convenience features

You are able to include contents of other YAML files into the one that you pass as an argument to the tool. To do so, you have to use the so called **$ref-syntax**. A description on how to use it can be found [here](https://swagger.io/docs/specification/using-ref/).
* The tool supports **Remote References** and **URL References**.
* **Local References** are not natively supported, but you can achieve the same effect by using **Anchors and Alisases** as they are stated in the YAML specification.


### Demo Files

In this [folder](cloud.foundry.cli/src/test/resources/demo/) we provide example YAML files that may help in
understanding how to use our tool. What can I use the files for?

* [getAll.yml](cloud.foundry.cli/src/test/resources/demo/getAll.yml): Example output of the **get** command. 
In this example you can see how you have to structure the yml Files in general.
* [refDemo.yml](cloud.foundry.cli/src/test/resources/demo/refDemo.yml) & 
[referred.yml](cloud.foundry.cli/src/test/resources/demo/referred.yml) Example how you can use the `$ref` syntax

__Happy coding!__
