from py4j.java_gateway import JavaGateway
from transformers import pipeline
import sys

summarizer = pipeline("summarization", model="google/flan-t5-base")

class SummarizerServer:
    def getSummary(self, text):
        # Determine min_length based on the length of the text
        min_length = 10 if len(text.split()) < 15 else 30
        
        # Adjust min_length in the summarizer call
        summary = summarizer(text, max_length=130, min_length=min_length, do_sample=False)
        
        # max number of words, min number of words, and returns highest probability words
        return summary[0]['summary_text']

if __name__ == "__main__":
    gateway = JavaGateway.launch_gateway()
    # Create an instance of SummarizerServer
    server = SummarizerServer()

    # Expose the server to Java through the gateway
    gateway.entry_point.server = server

    # Print the summary for the input text from the command line
    print(server.getSummary(sys.argv[1]))
