package com.thinkfirst.coach;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/** 数据库为会话的持久来源，正文只通过已验证归属的接口读取。 */
@Entity @Table(name="tf_conversation", indexes=@Index(name="idx_conversation_owner",columnList="ownerId"))
@Getter @Setter
public class StoredConversation {
    @Id private String id;
    @Column(nullable=false) private Long ownerId;
    @Column(nullable=false,length=500) private String title;
    private long updatedAt;
    @Lob @Column(nullable=false) private String snapshot;
}
