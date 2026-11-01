package com.thinkfirst.coach;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ConversationRepository extends JpaRepository<StoredConversation,String> {
    List<StoredConversation> findTop100ByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
