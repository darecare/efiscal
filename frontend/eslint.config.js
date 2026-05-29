import i18next from 'eslint-plugin-i18next'

export default [
  { ignores: ['dist/**', 'node_modules/**', 'scripts/**'] },
  {
    files: ['src/**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
    },
    ...i18next.configs['flat/recommended'],
    rules: {
      ...i18next.configs['flat/recommended'].rules,
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-text-only',
          'jsx-attributes': {
            include: ['title', 'placeholder', 'aria-label', 'alt'],
            exclude: [
              'className',
              'styleName',
              'style',
              'type',
              'key',
              'id',
              'width',
              'height',
              'value',
              'name',
              'htmlFor',
              'role',
              'to',
              'path',
              'href',
              'maxLength',
            ],
          },
        },
      ],
    },
  },
]
