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

Change the current directory to the JAR output folder \
```cf-control/cloud.foundry.cli/build/libs```.

Then you can run the __cf-control.jar__ by using the command line tool like: 
  
  ```java -jar cf-control.jar [COMMAND] [SUBCOMMAND] [PARAMS]``` 
  
##### [COMMAND]
```
  create          Create a new app, service or add a new space developer.
  get             Get the current configuration of your cf instance.
  diff            Print the differences between the given yaml file and the configuration of your cf instance.
  apply           Apply the configuration from a given yaml file to your cf instance.
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
   ```       
##### apply [SUBCOMMAND]
   ```
   applications      Create applications that are present in the given yaml file, but not in your cf instance.
   ```       
   
##### [PARAMS]
   ```
  -a, --api=<apiHost>                    Your CF instance's API endpoint URL.
  -o, --organization=<organization>      Your CF organization's name.
  -p, --password=<password>              Your password of your cf account.
  -s, --space=<space>                    Your CF space name.
  -u, --user=<userName>                  Your account's e-mail address or username.
  -y, --yaml=<yamlFile>                  The path to the yaml file. (Not needed for the get commands)
  ```


  for example, you can run the command: \
  ```java -jar cf-control.jar get services -a api.run.pivotal.io -o cloud.foundry.cli -s development -u mustermann@test.com -p somePassword;)```

### Convenience features
You are able to include contents of other YAML files into the one that you pass as an argument to the tool. To do so, you have to use the so called **$ref-syntax**. A description on how to use it can be found [here](https://swagger.io/docs/specification/using-ref/).
* The tool supports **Remote References** and **URL References**.
* **Local References** are not natively supported, but you can achieve the same effect by using **Anchors and Alisases** as they are stated in the YAML specification.

__Happy coding!__
