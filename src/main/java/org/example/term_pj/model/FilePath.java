package org.example.term_pj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "file_paths")
@NoArgsConstructor
@Getter
@Setter
public class FilePath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID uuid;

    private String filepath;

    private String filename;

    @Column(nullable = false)
    private LocalDateTime fileTime;

    private String extension;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usage_history_id", nullable = false)
    private UsageHistory usageHistory;

    public FilePath(UsageHistory usageHistory, String extension, String filename, String filepath,UUID uuid) {
        this.extension = extension;
        this.filename = filename;
        this.uuid = uuid;
        this.filepath = filepath;
        this.usageHistory = usageHistory;
        this.fileTime = LocalDateTime.now();
    }
}
