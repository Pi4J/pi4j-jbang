int loopCounter = 0;
int maxLoopCounter = 200;

int analogPinLightSensor = 0;

void setup() {
  Serial.begin(38400);
}

void loop() {
  if (Serial.available() > 0) {
    String received = Serial.readStringUntil('\n');

    Serial.print("{\"type\":\"echo\",\"value\":\"");
    Serial.print(received);
    Serial.println("\"}\n");
  }

  loopCounter++;

  if (loopCounter > maxLoopCounter) {
    Serial.print("{\"type\":\"light\",\"value\":");
    Serial.print(analogRead(analogPinLightSensor));
    Serial.println("}\n");
    
    loopCounter = 0;
  }
  
  delay(10);
}
