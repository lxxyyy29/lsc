<template>
  <PageContainer title="流程模板">
    <section class="panel process-template-edit-view">
      <h3>流程编辑已迁移到列表弹框</h3>
      <p>当前“查看 / 修改”均已回收到流程配置列表页的新建弹框中处理。</p>
      <div class="action-row">
        <button type="button" class="primary-button" @click="goBack">返回流程配置</button>
      </div>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PageContainer from '../../components/admin/PageContainer.vue'

const router = useRouter()

function goBack() {
  void router.push('/processes')
}
</script>

<style scoped>
@import '../admin-shared.css';

.process-template-edit-view {
  display: grid;
  gap: 16px;
}

.process-template-edit-view h3,
.process-template-edit-view p {
  margin: 0;
}

.process-template-edit-view h3 {
  color: #eef5ff;
}

.process-template-edit-view p {
  color: rgba(205, 222, 248, 0.78);
}

.action-row {
  margin-top: 8px;
}

</style>
