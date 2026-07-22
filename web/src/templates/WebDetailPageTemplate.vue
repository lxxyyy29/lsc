<template>
  <div class="web-detail-page-template" data-testid="web-detail-page-template">
    <section class="panel detail-shell detail-shell--hero">
      <div class="detail-shell__copy">
        <h3>{{ title }}</h3>
      </div>
      <div class="detail-shell__meta">
        <span class="detail-shell__code">{{ code }}</span>
        <slot name="hero-meta" />
      </div>
    </section>

    <section class="detail-overview">
      <article v-for="item in overviewItems" :key="item.label" class="panel detail-overview__card">
        <span class="detail-overview__label">{{ item.label }}</span>
        <strong class="detail-overview__value">{{ item.value }}</strong>
        <span v-if="item.hint" class="detail-overview__hint">{{ item.hint }}</span>
      </article>
    </section>

    <div class="detail-layout">
      <slot name="primary" />
      <slot name="aside" />
    </div>

    <div class="detail-layout">
      <slot name="secondary-primary" />
      <slot name="secondary-aside" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface OverviewItem {
  label: string
  value: string
  hint?: string
}

defineProps<{
  title: string
  code: string
  overviewItems: OverviewItem[]
}>()
</script>

<style scoped>
.detail-shell {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  padding: 24px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 16px;
  background: rgba(8, 30, 50, 0.5);
}

.detail-shell__copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-shell__copy h3 {
  margin: 0;
  color: var(--fg-text-primary, #eef5ff);
}

.detail-shell__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  min-width: 160px;
}

.detail-shell__code {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  font-size: 14px;
}

.detail-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.detail-overview__card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 12px;
  background: rgba(8, 30, 50, 0.5);
}

.detail-overview__label,
.detail-overview__hint {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

.detail-overview__value {
  color: var(--fg-text-primary, #eef5ff);
  font-size: 24px;
  line-height: 1.2;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 16px;
}

@media (max-width: 960px) {
  .detail-shell {
    flex-direction: column;
  }

  .detail-shell__meta {
    align-items: flex-start;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
