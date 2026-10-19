package com.example.tickets.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name="tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

}
