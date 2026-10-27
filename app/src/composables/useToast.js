import { ref } from 'vue'

const message = ref('')
let timer = null

export function useToast() {
  function toast(msg) {
    message.value = msg
    clearTimeout(timer)
    timer = setTimeout(() => {
      message.value = ''
    }, 4000)
  }

  return { message, toast }
}
