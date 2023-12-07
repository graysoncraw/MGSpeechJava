from py4j.java_gateway import JavaGateway, GatewayServer
from transformers import pipeline

summarizer = pipeline("summarization", model="t5-base")

class SummarizerServer:
    def getSummary(self, text):
        summary = summarizer(text, max_length=130, min_length=30, do_sample=False)
        return summary[0]['summary_text']

if __name__ == "__main__":
    gateway = GatewayServer(SummarizerServer())
    gateway.start()
    print("Py4J Gateway Server Started")
