<template>
  <main class="messages-page">
    <section class="page-header">
      <div>
        <span>站内交流</span>
        <h1>消息中心</h1>
      </div>
      <el-button :loading="loading" @click="loadConversations">刷新</el-button>
    </section>

    <section class="chat-shell">
      <aside class="conversation-list">
        <el-empty v-if="!conversations.length" description="暂无交流消息" />
        <button
          v-for="item in conversations"
          v-else
          :key="conversationKey(item)"
          class="conversation-item"
          :class="{ active: conversationKey(item) === conversationKey(activeConversation) }"
          type="button"
          @click="selectConversation(item)"
        >
          <span class="avatar">{{ getAvatarText(item.peer) }}</span>
          <span class="conversation-main">
            <strong>{{ getUserName(item.peer) }}</strong>
            <small>{{ item.targetTitle || '关联内容' }}</small>
            <em>{{ item.lastContent }}</em>
          </span>
          <el-badge v-if="item.unreadCount" :value="item.unreadCount" />
        </button>
      </aside>

      <section class="chat-panel">
        <div v-if="!activeConversation" class="empty-chat">
          <el-empty description="请选择一个会话开始交流" />
        </div>

        <template v-else>
          <header class="chat-header">
            <div>
              <strong>{{ getUserName(activeConversation.peer) }}</strong>
              <span>{{ activeConversation.targetTitle || '关联内容' }}</span>
            </div>
            <el-button size="small" :loading="messagesLoading" @click="loadMessages">刷新消息</el-button>
          </header>

          <div class="message-list" ref="messageListRef">
            <div
              v-for="message in messages"
              :key="message.messageId"
              class="message-row"
              :class="{ mine: message.mine }"
            >
              <div class="message-bubble">
                <p>{{ message.content }}</p>
                <time>{{ formatTime(message.createTime) }}</time>
              </div>
            </div>
          </div>

          <footer class="message-editor">
            <el-input
              v-model.trim="draft"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="输入要发送的消息"
              @keydown.ctrl.enter.prevent="sendMessage"
            />
            <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
          </footer>
        </template>
      </section>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatApi, getErrorMessage } from '../api'

const route = useRoute()
const loading = ref(false)
const messagesLoading = ref(false)
const sending = ref(false)
const conversations = ref([])
const activeConversation = ref(null)
const messages = ref([])
const draft = ref('')
const messageListRef = ref(null)

const conversationKey = (item) => {
  if (!item) return ''
  return `${item.peer?.userId || ''}-${item.targetType}-${item.targetId}`
}

const getUserName = (user) => user?.realName || user?.username || '用户'

const getAvatarText = (user) => {
  const name = getUserName(user)
  return name.slice(0, 1).toUpperCase()
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const loadConversations = async () => {
  loading.value = true
  try {
    const response = await chatApi.getConversations()
    if (response.code === 200) {
      conversations.value = response.data || []
      await activateFromQuery()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '消息加载失败'))
  } finally {
    loading.value = false
  }
}

const activateFromQuery = async () => {
  const peerId = Number(route.query.peerId)
  const targetType = route.query.targetType
  const targetId = Number(route.query.targetId)
  if (!peerId || !targetType || !targetId) {
    if (!activeConversation.value && conversations.value.length) {
      await selectConversation(conversations.value[0])
    }
    return
  }

  let found = conversations.value.find(item =>
    item.peer?.userId === peerId &&
    item.targetType === targetType &&
    Number(item.targetId) === targetId
  )

  if (!found) {
    found = {
      peer: {
        userId: peerId,
        username: route.query.peerName || '卖家'
      },
      targetType,
      targetId,
      targetTitle: route.query.targetTitle || '关联内容',
      lastContent: '',
      unreadCount: 0
    }
  }
  await selectConversation(found)
}

const selectConversation = async (item) => {
  activeConversation.value = item
  draft.value = ''
  await loadMessages()
}

const loadMessages = async () => {
  if (!activeConversation.value) return
  messagesLoading.value = true
  try {
    const response = await chatApi.getMessages({
      peerId: activeConversation.value.peer.userId,
      targetType: activeConversation.value.targetType,
      targetId: activeConversation.value.targetId
    })
    if (response.code === 200) {
      messages.value = response.data || []
      await scrollToBottom()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '消息加载失败'))
  } finally {
    messagesLoading.value = false
  }
}

const sendMessage = async () => {
  if (!activeConversation.value) return
  if (!draft.value) {
    ElMessage.warning('请输入消息内容')
    return
  }

  sending.value = true
  try {
    const response = await chatApi.sendMessage({
      receiverId: activeConversation.value.peer.userId,
      targetType: activeConversation.value.targetType,
      targetId: activeConversation.value.targetId,
      content: draft.value
    })
    if (response.code === 200) {
      messages.value.push(response.data)
      draft.value = ''
      await scrollToBottom()
      await loadConversations()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发送失败'))
  } finally {
    sending.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(loadConversations)
</script>

<style scoped>
.messages-page {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header,
.chat-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 18px;
}

.page-header span {
  color: #1f5eff;
  font-size: 13px;
  font-weight: 700;
}

.page-header h1 {
  margin: 6px 0 0;
  color: #172033;
  font-size: 28px;
}

.chat-shell {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  min-height: 640px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  overflow: hidden;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-right: 1px solid #e6ebf2;
  background: #fbfdff;
}

.conversation-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.conversation-item.active,
.conversation-item:hover {
  background: #edf4ff;
  border-color: #d6e5ff;
}

.avatar {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #ffffff;
  font-weight: 800;
  background: #1f5eff;
  border-radius: 50%;
}

.conversation-main {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.conversation-main strong,
.conversation-main small,
.conversation-main em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-main strong {
  color: #172033;
}

.conversation-main small {
  color: #64748b;
  font-size: 12px;
}

.conversation-main em {
  color: #667085;
  font-size: 13px;
  font-style: normal;
}

.chat-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}

.empty-chat {
  display: grid;
  place-items: center;
  min-height: 640px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-bottom: 1px solid #e6ebf2;
}

.chat-header div {
  display: grid;
  gap: 4px;
}

.chat-header strong {
  color: #172033;
}

.chat-header span {
  color: #64748b;
  font-size: 13px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 18px;
  overflow-y: auto;
  background: #f8fafc;
}

.message-row {
  display: flex;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-bubble {
  max-width: min(540px, 80%);
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.message-row.mine .message-bubble {
  color: #ffffff;
  background: #1f5eff;
  border-color: #1f5eff;
}

.message-bubble p {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble time {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.message-row.mine .message-bubble time {
  color: rgba(255, 255, 255, 0.76);
}

.message-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: flex-end;
  padding: 16px;
  border-top: 1px solid #e6ebf2;
}

@media (max-width: 760px) {
  .messages-page {
    padding: 16px;
  }

  .chat-shell {
    grid-template-columns: 1fr;
  }

  .conversation-list {
    max-height: 260px;
    overflow-y: auto;
    border-right: 0;
    border-bottom: 1px solid #e6ebf2;
  }

  .message-editor {
    grid-template-columns: 1fr;
  }
}
</style>
