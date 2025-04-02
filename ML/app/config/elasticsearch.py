from elasticsearch import Elasticsearch

def get_es_client():
    return Elasticsearch(
        "http://elasticsearch:9200",
        basic_auth=("fastapi_user", "e203@Password!"),
        verify_certs=False
    )
