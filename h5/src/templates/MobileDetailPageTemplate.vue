<template>
  <section class="mobile-detail-page-template" data-testid="mobile-detail-page-template">
    <header class="mobile-detail-page-template__header">
      <div class="mobile-detail-page-template__heading">
        <h1>{{ title }}</h1>
        <p
          v-if="subtitle"
          class="mobile-detail-page-template__subtitle"
          data-testid="mobile-detail-page-template-subtitle"
        >
          {{ subtitle }}
        </p>
      </div>
      <div
        v-if="$slots.summary"
        class="mobile-detail-page-template__summary"
        data-testid="mobile-detail-page-template-summary"
      >
        <slot name="summary" />
      </div>
    </header>

    <div class="mobile-detail-page-template__content">
      <slot />
    </div>

    <footer
      v-if="$slots.actions"
      class="mobile-detail-page-template__actions"
      data-testid="mobile-detail-page-template-actions"
    >
      <slot name="actions" />
    </footer>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.mobile-detail-page-template {
  display: grid;
  gap: 16px;
}

.mobile-detail-page-template__header,
.mobile-detail-page-template__actions {
  display: grid;
  gap: 16px;
}

.mobile-detail-page-template__heading {
  display: grid;
  gap: 4px;
}

.mobile-detail-page-template__heading h1,
.mobile-detail-page-template__subtitle {
  margin: 0;
}

.mobile-detail-page-template__heading h1 {
  font-size: 24px;
  line-height: 1.4;
  color: var(--fg-text-primary, #1f2a44);
}

.mobile-detail-page-template__subtitle {
  color: var(--fg-text-secondary, #5f6b85);
  line-height: 1.5;
}

.mobile-detail-page-template__content {
  display: grid;
  gap: 16px;
}

.mobile-detail-page-template__actions {
  position: sticky;
  bottom: 0;
}
</style>
