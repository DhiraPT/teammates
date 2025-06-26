package teammates.storage.sqlentity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang.StringUtils;

import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.SanitizationHelper;

/**
 * Represents a course.
 */
@Entity
@Table(name = "Courses")
public class Course extends BaseEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private String institute;

    @OneToMany(mappedBy = "course")
    private final List<FeedbackSession> feedbackSessions = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private final List<Section> sections = new ArrayList<>();

    private Instant deletedAt;

    protected Course() {
        // required by Hibernate
    }

    public Course(String id, String name, String timeZone, String institute) {
        this.setId(id);
        this.setName(name);
        this.setTimeZone(StringUtils.defaultIfEmpty(timeZone, Const.DEFAULT_TIME_ZONE));
        this.setInstitute(institute);
    }

    @Override
    public List<String> getInvalidityInfo() {
        List<String> errors = new ArrayList<>();

        addNonEmptyError(FieldValidator.getInvalidityInfoForCourseId(getId()), errors);
        addNonEmptyError(FieldValidator.getInvalidityInfoForCourseName(getName()), errors);
        addNonEmptyError(FieldValidator.getInvalidityInfoForInstituteName(getInstitute()), errors);

        return errors;
    }

    /**
     * Adds a section to the Course.
     */
    public void addSection(Section section) {
        this.sections.add(section);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = SanitizationHelper.sanitizeTitle(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = SanitizationHelper.sanitizeName(name);
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = SanitizationHelper.sanitizeTitle(institute);
    }

    public List<FeedbackSession> getFeedbackSessions() {
        return Collections.unmodifiableList(feedbackSessions);
    }

    public void setFeedbackSessions(List<FeedbackSession> feedbackSessions) {
        this.feedbackSessions.clear();
        if (feedbackSessions != null) {
            this.feedbackSessions.addAll(feedbackSessions);
        }
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }

    public void setSections(List<Section> sections) {
        this.sections.clear();
        if (sections != null) {
            this.sections.addAll(sections);
        }
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        if (deletedAt != null && (this.getCreatedAt() == null || deletedAt.isBefore(this.getCreatedAt()))) {
            throw new IllegalArgumentException("Deleted time cannot be before creation time.");
        }
        this.deletedAt = deletedAt;
    }

    public boolean isCourseDeleted() {
        return this.deletedAt != null;
    }

    @Override
    public String toString() {
        return "Course [id=" + id + ", name=" + name + ", timeZone=" + timeZone + ", institute=" + institute
                + ", feedbackSessions=" + feedbackSessions + ", sections=" + sections + ", deletedAt=" + deletedAt
                + ", " + super.toString() + "]";
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Course otherCourse = (Course) other;
        return Objects.equals(id, otherCourse.id);
    }
}
