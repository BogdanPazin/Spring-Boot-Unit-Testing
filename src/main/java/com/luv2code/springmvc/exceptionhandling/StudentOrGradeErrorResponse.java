package com.luv2code.springmvc.exceptionhandling;

// OVA KLASA JE DATA TRANSFER OBJECT
// ONA IDE UZ ResponseEntity<>, GDE JE SAM DTO BODY TOG RESPONSE-A
public class StudentOrGradeErrorResponse {

    private int status;
    private String message;
    private long timeStamp;

    public StudentOrGradeErrorResponse() {}

    public StudentOrGradeErrorResponse(int status, String message, long timeStamp) {
        this.status = status;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
