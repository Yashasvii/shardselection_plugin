# ShardSelection
This is the plugin that estimate the relevant shard for a given query. We can apply four algorithms via this plugin namely ReDDe, SUSHI, Rank-S and newly developed algorithm called Hybrid Optimized Shard Selection Algorithm.
The plugin need to be installed at all the data nodes and broker nodes.

#Configuring the cluster
The plugin expect atleast one broker node(which is called coordinating node in Elastisearch) and two or more data nodes.

#Configuration 
In all the nodes, configuration properties, set: <br/> <br />
shardselection.enabled:true
shardselection.sync: true <br />
shardselection.interval: 10m <br />
shardselection.fields: [field1, field2] <br />
shardselection.datanode: true <br />
shardselection.brokernode: false


The above configuration enable the shard selection, enable the sync between data nodes and broker nodes in the intervals of 10 minutes and specify the fields for the shard selection.


#Searching
A separate REST request is developed for the search. The rest request looks like following:

curl -XPOST http://localhost:8080/shardSelection
```json
{
"algorithm": "Hybrid",
"maxShards":20,
"totalShards":20,
"search_query" : {
 "query": {
    "match": {
     "content": "players coaches"
    }
  }
},
"indexName":"http",
"alpha":20,
"routingFields": null

}
```

Here,
"algorithm" takes values of ["Redde", "Sushi", "RankS", "hybrid" ] <br/>
"maxShards" is the maximum number of shards to be considered for shard selection <br/>
"totalShards" is the total number of shard for the index to be queried <br/>
"search_query" is the query to be executed <br/>
"indexName" name of the index </br>
"alpha": a tunable numeric constant to determine how many documents are considered for the voting of their priority <br/>
"routingField": Fields to be considered for query routing(if any)

Dataset used for experimentation is Insider Threat Test Dataset(CERT V6.2) email.csv and http.csv from the site:

https://kilthub.cmu.edu/articles/dataset/Insider_Threat_Test_Dataset/12841247/1




