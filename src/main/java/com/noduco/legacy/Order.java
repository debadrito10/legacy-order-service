package com.noduco.legacy;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
public class Order {
    private String id;
    private String customer;
    private double amount;
    private Date createdAt;

    public Order() { }   // JAXB and Jackson need a no-argument constructor

    public Order(String id, String customer, double amount) {
        this.id = id;
        this.customer = customer;
        this.amount = amount;
        this.createdAt = new Date();
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public double getAmount() { return amount; }
    public Date getCreatedAt() { return createdAt; }
}


