FROM python:alpine3.7
COPY . /app
WORKDIR /app
RUN pip3 install -r requirements.txt
EXPOSE 8080
EXPOSE 8081
CMD python3 app.py --port 8080 --monitor 8081