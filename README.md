# cf-control
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
```
  
##### create [SUBCOMMAND]
```
  service          Create a service in the target space.
  space-developer  Assign users as space developers.
  application      Create a application in the target space.
```
    
##### get [SUBCOMMAND]
   ```
   services          List all services in the target space.
   space-developers  List all space developers in the target space.
   applications      List all applications in the target space.
   all               show all information in the target space
   ```       
        
##### [PARAMS]
   ```
  -a, --api=<apiHost>                    Your CF instance's API endpoint URL.
  -o, --organization=<organization>      Your CF organization's name.
  -p, --password=<password>              Your password of your cf account.
  -s, --space=<space>                    Your CF space name.
  -u, --user=<userName>                  Your account's e-mail address or username.
  ```


  for example, you can run the command: \
  ```java -jar cf-control.jar get services -a api.run.pivotal.io -o cloud.foundry.cli -s development -u mustermann@test.com -p somePassword;)```


__Happy coding!__