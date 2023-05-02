package ru.example.SpringDemoBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.SpringDemoBot.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
