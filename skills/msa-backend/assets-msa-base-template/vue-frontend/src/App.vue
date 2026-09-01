<script setup>
import { ref, onMounted } from 'vue'
import { resourceApi } from './api'

const resources = ref([])
const error = ref('')
const newItem = ref({ name: '', description: '' })

function unwrap(res) {
  // 백엔드 공통 응답 래퍼 { success, message, data } 처리
  const body = res.data
  return body && body.data !== undefined ? body.data : body
}

async function load() {
  try {
    resources.value = unwrap(await resourceApi.list())
  } catch (e) {
    error.value = '조회 실패: ' + e.message
  }
}

async function create() {
  try {
    await resourceApi.create({ ...newItem.value })
    newItem.value = { name: '', description: '' }
    await load()
  } catch (e) {
    error.value = '생성 실패: ' + e.message
  }
}

onMounted(load)
</script>

<template>
  <main class="wrap">
    <h1>MSA Base Template</h1>
    <p class="hint">service-template 의 /api/resources 를 호출하는 최소 예시 화면입니다.</p>
    <p v-if="error" class="error">{{ error }}</p>

    <div class="card">
      <h2>Resources</h2>
      <form @submit.prevent="create">
        <input v-model="newItem.name" placeholder="name" required />
        <input v-model="newItem.description" placeholder="description" />
        <button type="submit">생성</button>
      </form>
      <ul>
        <li v-for="r in resources" :key="r.id">#{{ r.id }} {{ r.name }} — {{ r.description }}</li>
      </ul>
    </div>
  </main>
</template>

<style>
body { margin: 0; font-family: system-ui, sans-serif; background: #f5f6f8; color: #1c1c1e; }
.wrap { max-width: 700px; margin: 0 auto; padding: 32px 20px; }
h1 { margin: 0 0 4px; }
.hint { color: #666; margin-top: 0; }
.error { color: #c0392b; }
.card { background: #fff; border: 1px solid #e3e5e8; border-radius: 10px; padding: 16px; }
form { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
input { padding: 8px; border: 1px solid #ccc; border-radius: 6px; }
button { padding: 8px; border: none; border-radius: 6px; background: #2d6cdf; color: #fff; cursor: pointer; }
button:hover { background: #1e57bd; }
ul { padding-left: 18px; margin: 0; }
li { padding: 2px 0; }
</style>
