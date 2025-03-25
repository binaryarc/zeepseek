import logging

logger = logging.getLogger("my_app_logger")
logger.setLevel(logging.INFO)

if not logger.handlers:
    stream_handler = logging.StreamHandler()
    fmt = "[%(levelname)s] %(asctime)s - %(message)s"
    formatter = logging.Formatter(fmt)
    stream_handler.setFormatter(formatter)
    logger.addHandler(stream_handler)
