from py4j.java_gateway import JavaGateway
from transformers import pipeline
import sys

summarizer = pipeline("summarization", model="google/flan-t5-base")

class SummarizerServer:
    def getSummary(self, text):
        summary = summarizer(text, max_length=130, min_length=30, do_sample=False)
        # max number of words, min number of words, and returns highest probability words
        return summary[0]['summary_text']

if __name__ == "__main__":
    gateway = JavaGateway.launch_gateway()
    # Create an instance of SummarizerServer
    server = SummarizerServer()

    # Expose the server to Java through the gateway
    gateway.entry_point.server = server

    #print("Py4J Gateway Server Started. Waiting for requests...")

    print(server.getSummary(sys.argv[1]))
