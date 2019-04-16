curl -X POST --metaData 'Content-Type: application/json' --metaData 'Accept: application/json' -d '{
    "class" : "uk.gov.gchq.gaffer.operation.impl.add.AddElements",
    "input" : [ {
     "group" : "BasicEntity",
     "vertex" : "1",
     "properties" : {
       "count" : 1
     },
     "class" : "uk.gov.gchq.gaffer.data.element.Entity"
    }, {
     "group" : "BasicEdge",
     "source" : "1",
     "destination" : "2",
     "directed" : true,
     "properties" : {
       "count" : 1
     },
     "class" : "uk.gov.gchq.gaffer.data.element.Edge"
    } ],
     "options": {
      "gaffer.federatedstore.operation.graphIds": "accEntities,mapEdges"
      }
 }' 'http://localhost:8080/rest/v2/graph/operations/execute'
