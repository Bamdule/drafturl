package com.drafturl.api.domain.user.entity;

import com.drafturl.api.global.common.BaseEntity;
import com.drafturl.api.domain.user.PlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_provider_id",
                columnNames = {"provider", "providerId"}
        )
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    private String profileImage;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType plan = PlanType.FREE;

    protected User() {
    }

    public User(String email, String name, String profileImage, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.provider = provider;
        this.providerId = providerId;
        this.plan = PlanType.FREE;
    }

    public User(String email, String name, String password, String provider, String providerId, boolean withPassword) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.plan = PlanType.FREE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getPassword() {
        return password;
    }

    public PlanType getPlan() {
        return plan;
    }

    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }
}
