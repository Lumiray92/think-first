package com.thinkfirst.coach;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
/**
 * 会话存储边界：模型生成成功后才序列化完整会话，事务提交失败会向前端报告错误。
 * 密码与API密钥不属于会话字段，不会写入这里的JSON快照。
 * 数据库查询和内存缓存分离，服务重启后可以反序列化恢复当前卡片。
 * 账号归属由CoachService在返回数据或执行动作前再次校验。
 */
@Service
public class ConversationStore {
    private final ConversationRepository repository;
    private final ObjectMapper mapper;
    public ConversationStore(ConversationRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Transactional
    public void save(CoachSession session){
        try {
            StoredConversation row = new StoredConversation();
            row.setId(session.getId());row.setOwnerId(session.getOwnerId());row.setTitle(session.getIdea());
            row.setUpdatedAt(session.getUpdatedAt());row.setSnapshot(mapper.writeValueAsString(session));repository.save(row);
        } catch(Exception e){throw new IllegalStateException("会话保存失败，请重试",e);}
    }
    @Transactional(readOnly=true)
    public CoachSession load(String id){
        StoredConversation row=repository.findById(id).orElseThrow(()->new CoachSessionNotFoundException(id));
        try{return mapper.readValue(row.getSnapshot(),CoachSession.class);}catch(Exception e){throw new IllegalStateException("会话读取失败",e);}
    }
    @Transactional(readOnly=true)
    public List<CoachSession> list(Long ownerId){return repository.findTop100ByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(row->load(row.getId())).toList();}
}
