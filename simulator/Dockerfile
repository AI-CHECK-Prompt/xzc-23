FROM python:3.11-alpine
WORKDIR /app
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple requests
COPY simulator.py .
CMD ["python", "simulator.py"]
