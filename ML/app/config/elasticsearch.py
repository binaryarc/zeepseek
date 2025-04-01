from elasticsearch import Elasticsearch

def get_es_client():
    return Elasticsearch(
        "http://elasticsearch:9200",
        basic_auth=("kibana", "2zgdxAdWzbURMTj0YMwu"),
        verify_certs=False
    )
