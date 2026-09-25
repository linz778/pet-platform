import { afterAll, beforeAll, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import Hall from './Hall.vue'
import { getMySitterProfile, listMyAddresses, listPetNotes, pageMyTakenOrders, pageHallOrders } from '@/api/sitter'
import { getCurrentPosition } from '@/utils/amap'

vi.mock('vue-router', async (importOriginal) => ({
  ...await importOriginal(),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/utils/amap', () => ({
  getCurrentPosition: vi.fn(),
  searchAdministrativeChildren: vi.fn(),
  searchPois: vi.fn()
}))
vi.mock('@/api/sitter', () => ({
  getMySitterProfile: vi.fn(), listMyAddresses: vi.fn(), listPetNotes: vi.fn(),
  pageMyTakenOrders: vi.fn(), pageHallOrders: vi.fn(),
  createAddress: vi.fn(), deleteAddress: vi.fn(), deletePetNote: vi.fn(), grabOrder: vi.fn(),
  savePetNote: vi.fn(), setDefaultAddress: vi.fn(), submitSitterProfile: vi.fn(), updateAddress: vi.fn()
}))

beforeAll(() => vi.stubEnv('VITE_AMAP_KEY', 'test-key'))
afterAll(() => vi.unstubAllEnvs())

it('地图与待接订单并列，两个详情按钮各自打开弹窗', async () => {
  getMySitterProfile.mockResolvedValue({ auditStatus: 1, auditStatusText: '已通过' })
  listMyAddresses.mockResolvedValue([])
  listPetNotes.mockResolvedValue([])
  pageMyTakenOrders.mockResolvedValue({ records: [] })
  pageHallOrders.mockResolvedValue({
    total: 1,
    records: [{
      id: 1, orderNo: 'P-TEST-1', categoryName: '上门喂养', categoryCode: 'FEEDING',
      petName: '奶糖', petSpecies: '猫', serviceStart: '2026-09-23 10:00:00',
      serviceAddress: '上海市测试路 1 号', amount: 40, sitterIncome: 36, distanceKm: 0.5
    }]
  })
  getCurrentPosition.mockResolvedValue({ lng: 121.4737, lat: 31.2304 })
  const wrapper = shallowMount(Hall, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        ElCard: { template: '<div><slot /></div>' },
        ElButton: { template: '<button @click="$emit(\'click\', $event)"><slot /></button>' },
        ElDialog: { props: ['modelValue', 'title'], template: '<div v-if="modelValue" role="dialog"><h2>{{ title }}</h2><slot /><slot name="footer" /></div>' },
        ElDescriptions: { template: '<div><slot /></div>' },
        ElDescriptionsItem: { template: '<div><slot /></div>' },
        AmapView: { template: '<div class="fake-map" />' }
      },
      directives: {}
    }
  })
  await flushPromises()

  expect(getMySitterProfile).toHaveBeenCalled()
  expect(wrapper.find('.hall-main-grid').exists()).toBe(true)
  expect(wrapper.find('.hall-main-grid .map-card').exists()).toBe(true)
  expect(wrapper.find('.hall-main-grid .orders-card').exists()).toBe(true)
  await wrapper.findAll('button').find((button) => button.text() === '订单详情').trigger('click')
  expect(wrapper.find('[role="dialog"]').text()).toContain('P-TEST-1')
  expect(wrapper.find('[role="dialog"]').text()).toContain('奶糖')

  await wrapper.findAll('button').find((button) => button.text() === '关闭').trigger('click')
  await wrapper.findAll('button').find((button) => button.text() === '地图详情').trigger('click')
  expect(wrapper.find('[role="dialog"]').text()).toContain('附近订单地图详情')
  wrapper.unmount()
})
