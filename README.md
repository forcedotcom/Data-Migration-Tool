# Data-Migration-Tool (Datafall)

*Data-Migration-Tool (Datafall)* is a tool that migrates the data from one Salesforce Org to another Salesforce Org.

Migrating data between orgs can be a daunting ask, this is specially true if you have a complex data model in the form of parent-child, hierarchical, lookups, record types etc relationships.

Doing so manually is error prone, time consuming, frustrating and not an ideal approach. We wrote a Java based tool using Salesforce SOAP to migrate the data from one Salesforce Org to another Salesforce Org. Data-Migration-Tool tool lets you migrate the data from one Salesforce Org to another Salesforce Org. This is a standalone java tool that one can run from a local machine. 

## Build Data Migrator

#### Tool has two modules.

* datamigrator-core: This module has tools core classes
* datamigrator-module: This module has sample examples on how the tool can be used to migrate different kind of related objects from one Salesforce org to another Salesforce org. *datamigrator-module* contains sample JSON object relationship mappings.

#### Build
    git clone git@github.com:forcedotcom/Data-Migration-Tool.git
	cd Data-Migration-Tool
	mvn clean install

#### Usage: Migrate Products, Pricebooks and PricebookEntries Sample

	# Enter Source and Target org credentials in `/datamigrator-module/src/main/resources/build.properties`
	mvn clean install
	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateProducts" -pl datamigrator-module

## Running in your IDE

### Overview

You can load the project into your IDE and run the main class to kick-off the migration. We recommend using [Eclipse](http://www.eclipse.org/downloads/). Because this tool is a standard Maven project, you can import it into your IDE using the root `pom.xml` file. In Eclipse, choose Open from the File menu -> Import -> Maven -> and select `Existing Maven Projects` -> select the the root folder.

After opening the project in Eclipse, you will see below two modules:

* datamigrator-core: This module has tools core classes
* datamigrator-module: Contains the mapping and main java classes to run the migration

Tool comes with many main classes as a way to show different kind of mappings and migrations:

* Main Class: `migrator.module.client.MigrateProducts`
* Main Class: `migrator.module.client.MigrateAll`
* Main Class: `migrator.module.client.MigrateAccounts`
* Main Class: `migrator.module.client.MigrateAccountsHierarchical`

and many more...


## Running using command line (More Sample Migrations...)
To avoid creating duplicates in the target org, use [external ids](https://github.com/forcedotcom/Data-Migration-Tool/wiki/External-Id). This section has some examples using external ids.

Migrate a simple object's records (no relationships)

	# Edit the object-mappings/Products.json with your object API name, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateOnlyProducts" -pl datamigrator-module

Migrate an object with lookups (Pricebook2 with lookup to Product) relationship

	# Edit the object-mappings/Products.json, PricebookEntry_nonstandard.json, PricebookEntry_standard.json with your object API name, save.

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateProducts" -pl datamigrator-module

Migrate an object with lookups (Pricebook2 with lookup to Product) relationship with External Ids

	# Edit the object-mappings/Products_extId.json, PricebookEntry_nonstandard_extId.json, PricebookEntry_standard_extId.json with your object API name, save.

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateProductsWithExternalId" -pl datamigrator-module

Migrate object's with masterdetail (Accounts/Assets/Oppties) relationships

	# Edit the object-mappings/AccountwithAssetsAndOppties.json with your object API names, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateAccounts" -pl datamigrator-module

Migrate object's with hierarchical relationships

	# Edit the object-mappings/AccountwithAssetsAndOppties_hierarchical.json with your object API names, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateAccountsHierarchical" -pl datamigrator-module

Migrate multiple object's in a single run

	# Edit the object-mappings as referenced in MigrateAll.java with your object API names, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateAll" -pl
	datamigrator-module

Migrate Accounts with Opportunities and Assets. Make sure you edit the JSON with your external ids from Accounts with Opportunities and Assets objects.

	# Edit the object-mappings/AccountwithAssetsAndOppties_hierarchical.json with your object's external Id API name, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.MigrateAccountsHierarchical" -pl datamigrator-module


## Usage (Sample Deletions from Target Org)

Delete a simple object's records

	# Edit the object-mappings/Products.json with your object API name, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.DeleteProducts" -pl datamigrator-module

Delete multiple object's records in a single run

	# Edit the object-mappings as referenced in DeleteAll.java with your object API names, save

	mvn exec:java -Dexec.mainClass="migrator.module.client.DeleteAll" -pl datamigrator-module


## Usage (Compare objects metadata in source and target org)

Compare objects Metadata in source and target org

	mvn exec:java -Dexec.mainClass="migrator.module.client.MetadataCompareExample" object1,object2 -pl datamigrator-module

## Usage (Given a set of objects, create a JSON mapping structure)

Given a set of objects, create a JSON mapping structure

	mvn exec:java -Dexec.mainClass="migrator.module.client.GenerateMappingExample" object1,object2 -pl datamigrator-module

## Usage (Counts the number of records in source and target org)

Once migration has completed, you may want to compare the number of records in source and target org

	mvn exec:java -Dexec.mainClass="migrator.module.client.ValidateExample" object1,object2 -pl datamigrator-module


# More Advanced Object Relationship JSON Mappings, Resources

For more information, see the [wiki](https://github.com/forcedotcom/Data-Migration-Tool/wiki)

## Authors

Anoop Singh by [Anoop Singh](mailto:anoop.singh@salesforce.com, anoop_76@yahoo.com)
