package site.buterr.board;

public class Message {
    // TODO 1: 定义字段（建议使用不可变：private final String nickname/content）
    private final String nickname;
    private final String content;

    // TODO 2: 构造方法，接收 nickname 与 content 并赋值
    public Message(String nickname, String content) {
        // TODO
        this.nickname = nickname;
        this.content = content;
    }

    // TODO 3: 提供 getter（无需 setter）
    public String getNickname() {
        return nickname ; // TODO
    }

    public String getContent() {
        return content; // TODO
    }
}
