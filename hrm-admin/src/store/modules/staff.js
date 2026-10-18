import router, { resetRouter } from '../../router'
import { close as closeSse } from '../../utils/sse'

const STAFF_ID_KEY = 'staffId'

export default {
  namespaced: true,
  state: {
    staff: null
  },
  mutations: {
    SET_STAFF (state, staff) {
      state.staff = staff
      if (staff && staff.id) {
        sessionStorage.setItem(STAFF_ID_KEY, String(staff.id))
      } else {
        sessionStorage.removeItem(STAFF_ID_KEY)
      }
    },
    SET_AVATAR (state, avatar) {
      if (state.staff) {
        state.staff.avatar = avatar
      }
    }
  },
  actions: {
    logout ({ commit }) {
      closeSse()
      localStorage.clear()
      sessionStorage.removeItem(STAFF_ID_KEY)
      commit('token/SET_AUTH', false, { root: true })
      commit('menu/CLEAR_MENU', null, { root: true })
      commit('tag/CLEAR_TAG', null, { root: true })
      resetRouter()
      router.push({ name: 'login' }).then(() => {})
    }
  }
}
