# InstDB

This software  offers service capabilities such as data publishing, data discovery, and data transmission. The data publishing service enables users to register datasets for sharing, allowing data stewards to review sharing requests and supporting shared dataset updates. The data discovery service provides category navigation, metadata retrieval, classification filtering, and recommendations of datasets. The data transmission service supports various data transport protocols such as NFS, FTP, BT, etc. It also facilitates fragmented and breakpoint-resume transmission of large-scale datasets and ensures integrity verification of delivered datasets.


## build & install
```shell
mvn clean compile install 
```

## using instdb
add repository in pom.xml:
```xml
<dependency>
    <groupId>cn.cnic</groupId>
    <artifactId>instdb</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Example：http://127.0.0.1

## System Overview

Scientific data publishing software is designed to meet the data disclosure needs of institutional researchers and research teams. It provides academic review support for scientific data or other academic achievements, allowing scientific data to be published according to the established permissions and permits of the publisher. Once the data is published, it is registered with two permanent and unique identifiers, DOI and CSTR, to facilitate the collection and submission of scientific researchers and research teams according to project, publishing institution or institutional management requirements Publish data to determine data rights and provide support.
The main features are as follows:

Scientific data academic publishing: Research institutions can customize metadata standards for specific needs of their fields and research institutions, and implement academic reviews according to the general process of publishing and quality management of scientific achievements. The published data resources provide discovery functions such as metadata based retrieval, hot word recommendation, and ranking of success rates, supporting the organization of data resources as research hotspots, project topics, and other topics;

Customization of shared permissions: The published scientific data resources can be accessed according to the policies formulated by the publisher, with access policies, protection periods, restrictions, or prohibitions;

Permanent unique identification registration: Connect with the unified identification registration service of the central center, providing DOI and CSTR recognized unique identification registration for data resources both domestically and internationally;

Interoperability support: Deeply integrate with online publishing of data collaborative management tools, implement the standard interface requirements of the institute's data center system, and provide various standard API interface services such as data publishing and data access based on access authorization and token security policies;

## Server rapid deployment
Provide two deployment methods: traditional deployment and containerized deployment
### Traditional deployment
1. Download and deploy the installation package instdb-0.0.1-SNAPSHOT.jar at the download address http://127.0.0.1:3000/wangdongdong/instdb
2. Log in to the deployment machine, copy the jar package to the deployment path, and execute:
```shell
nohup java -jar instdb-0.0.1-SNAPSHOT.jar >instdb.log 2>&1 &
```
4. start-upserver
```shell
sh start.sh
```
5. View server running logs
```~~shell~~
tail -f ./logs/instdb.log
```
6. Stop server
```shell
sh stop.sh
```

### Containerized deployment
1. Write docker-compose.yml
```yml
version: '3.6'

services:
  mongo:
    image: fairmarket.casdc.cn/instdb/mongo:v1.0.0
    volumes:
      - type: "bind"
        source: "/mnt/data/fairman/InstDB/mongo"
        target: "/data/db"
        desc: "Mongo data persistence storage path (please create this path on the server in advance)"
  elasticsearch:
    image: fairmarket.casdc.cn/instdb/elasticsearch:v1.0.0
    volumes:
      - type: "bind"
        source: "/mnt/data/fairman/InstDB/elasticsearch"
        target: "/data/elasticsearch-6.8.10/data"
        desc: "Elasticsearch data persistence storage path (please create this path on the server in advance)"
  api:
    image: fairmarket.casdc.cn/instdb/api:v1.0.0
    ports:
      - target: 8080
        published: 8081
        protocol: tcp
        desc: "Homepage Port"
      - target: "50200-50300"
        published: "50200-50300"
        protocol: tcp
        desc: "FTP data transmission port"
      - target: 2121
        published: 2121
        protocol: tcp
        desc: "FTP connection port"
    volumes:
      - type: "bind"
        source: "/mnt/data/fairman/InstDB/instdb"
        target: "/data/instdb"
        desc: "Static file persistent storage path"
    environment:
      - key: "ACC_HOST"
        val: "127.0.0.1"
        desc: "Home IP"
      - key: "ACC_PORT"
        val: "8081"
        desc: "Homepage Port"
      - key: "FTP_PORT1"
        val: "2121"
        desc: "FTP port"
    depends_on:           
      - mongo
      - elasticsearch
```
2. Start Service
```shell
docker-compose up -d
```
3. stop service
```shell
docker-compose down
```
