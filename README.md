# [Nicks.Guru](https://nicks.guru) Commons Feature Toggle Starter

Starter for making Spring beans functional/non-functional depending on feature states.

## Usage

Pick the most recent version from
[Maven Central](https://central.sonatype.com/namespace/guru.nicks.commons), then use as follows:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>guru.nicks.commons</groupId>
            <artifactId>bom</artifactId>
            <version>1.17.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>guru.nicks.commons</groupId>
        <artifactId>feature-toggle-starter</artifactId>
    </dependency>
</dependencies>
```

## Documentation

To browse the API documentation, click [here](https://nicks.guru/commons/commons-feature-toggle-starter/apidocs).

## Disclaimer

THIS CODE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. USE AT YOUR OWN RISK.

Copyright © 2025 [nicks.guru](https://nicks.guru). All rights reserved.
