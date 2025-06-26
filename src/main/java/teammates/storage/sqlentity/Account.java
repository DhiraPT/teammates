package teammates.storage.sqlentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import teammates.common.util.FieldValidator;
import teammates.common.util.SanitizationHelper;

/**
 * Represents a unique account in the system.
 */
@Entity
@Table(name = "Accounts")
public class Account extends BaseEntity {
    @Id
    private UUID id;

    @NaturalId
    private String googleId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private final List<ReadNotification> readNotifications = new ArrayList<>();

    protected Account() {
        // required by Hibernate
    }

    public Account(String googleId, String name, String email) {
        this.setId(UUID.randomUUID());
        this.setGoogleId(googleId);
        this.setName(name);
        this.setEmail(email);
    }

    /**
     * Add a read notification to this account.
     */
    public void addReadNotification(ReadNotification readNotification) {
        readNotifications.add(readNotification);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = SanitizationHelper.sanitizeGoogleId(googleId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = SanitizationHelper.sanitizeName(name);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = SanitizationHelper.sanitizeEmail(email).toLowerCase();
    }

    public List<ReadNotification> getReadNotifications() {
        return Collections.unmodifiableList(readNotifications);
    }

    public void setReadNotifications(List<ReadNotification> readNotifications) {
        this.readNotifications.clear();
        if (readNotifications != null) {
            this.readNotifications.addAll(readNotifications);
        }
    }

    @Override
    public List<String> getInvalidityInfo() {
        List<String> errors = new ArrayList<>();

        addNonEmptyError(FieldValidator.getInvalidityInfoForGoogleId(googleId), errors);
        addNonEmptyError(FieldValidator.getInvalidityInfoForPersonName(name), errors);
        addNonEmptyError(FieldValidator.getInvalidityInfoForEmail(email), errors);

        return errors;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Account otherAccount = (Account) other;
        return Objects.equals(id, otherAccount.id);
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public String toString() {
        return "Account [id=" + id + ", googleId=" + googleId + ", name=" + name + ", email=" + email
                + ", readNotifications=" + readNotifications + ", " + super.toString() + "]";
    }
}
