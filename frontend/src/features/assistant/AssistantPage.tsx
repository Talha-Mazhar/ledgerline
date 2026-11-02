import ArrowUpwardRounded from '@mui/icons-material/ArrowUpwardRounded';
import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { askAssistantMutation } from '@/shared/api/generated/@tanstack/react-query.gen';
import { problemMessage, problemStatus } from '@/shared/api/problem';
import { PageHeader } from '@/shared/ui/PageHeader';

const MAX_LENGTH = 500;

const suggestions = [
  'Where did my money go this month?',
  'How much did I spend last month?',
  'What are my balances?',
];

interface Exchange {
  readonly question: string;
  readonly answer: string;
}

/**
 * "Where did my money go?" A language model answers questions about the customer's own money.
 * It runs on Ledgerline's servers (Ollama), not a third-party API, and it can only see this
 * customer's data through read-only tools the backend gives it: balances and activity. It never
 * moves money.
 */
export function AssistantPage() {
  const [question, setQuestion] = useState('');
  const [conversation, setConversation] = useState<readonly Exchange[]>([]);
  const ask = useMutation({
    ...askAssistantMutation(),
    onSuccess: ({ answer }, { body }) => {
      setConversation((earlier) => [...earlier, { question: body.question, answer }]);
      setQuestion('');
    },
  });

  function send(text: string) {
    const trimmed = text.trim();
    if (trimmed.length > 0 && !ask.isPending) {
      ask.mutate({ body: { question: trimmed } });
    }
  }

  const offline = problemStatus(ask.error) === 503;

  return (
    <>
      <PageHeader
        title="Assistant"
        description="Ask about your money in plain words. It reads your balances and activity; it can't move money."
      />

      <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, maxWidth: 820 }}>
        <Stack spacing={2.5} aria-live="polite" sx={{ mb: 3 }}>
          {conversation.length === 0 && (
            <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
              <AutoAwesomeOutlined sx={{ color: 'primary.main', mt: 0.25 }} aria-hidden />
              <Typography sx={{ color: 'text.secondary' }}>
                Try one of these, or ask your own question.
              </Typography>
            </Box>
          )}
          {conversation.map((exchange, index) => (
            <Box key={index} sx={{ display: 'grid', gap: 1.25 }}>
              <Typography
                sx={{
                  justifySelf: 'end',
                  maxWidth: '80%',
                  bgcolor: 'ledger.ink',
                  color: 'ledger.onInk',
                  px: 2,
                  py: 1.25,
                  borderRadius: 2,
                }}
              >
                {exchange.question}
              </Typography>
              <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', maxWidth: '90%' }}>
                <AutoAwesomeOutlined sx={{ color: 'primary.main', mt: 0.25 }} aria-hidden />
                <Typography sx={{ whiteSpace: 'pre-line' }}>{exchange.answer}</Typography>
              </Box>
            </Box>
          ))}
          {ask.isPending && (
            <Typography sx={{ color: 'text.secondary', pl: 4.5 }}>
              Looking at your ledger…
            </Typography>
          )}
        </Stack>

        {ask.isError && (
          <Alert severity={offline ? 'info' : 'error'} sx={{ mb: 2 }}>
            {offline
              ? "The assistant is offline right now. Its model runs on our own servers and isn't started in this environment."
              : problemMessage(ask.error, "The assistant didn't answer. Try again in a moment.")}
          </Alert>
        )}

        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap', mb: 2 }}>
          {suggestions.map((suggestion) => (
            <Chip
              key={suggestion}
              label={suggestion}
              variant="outlined"
              onClick={() => send(suggestion)}
              disabled={ask.isPending}
            />
          ))}
        </Stack>

        <Box
          component="form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            send(question);
          }}
          sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}
        >
          <TextField
            label="Your question"
            fullWidth
            multiline
            maxRows={4}
            value={question}
            onChange={(event) => setQuestion(event.target.value.slice(0, MAX_LENGTH))}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                send(question);
              }
            }}
            helperText={`${question.length}/${MAX_LENGTH}`}
          />
          <IconButton
            type="submit"
            aria-label="Ask"
            disabled={question.trim().length === 0 || ask.isPending}
            sx={{
              mt: 1,
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              '&:hover': { bgcolor: 'primary.dark' },
            }}
          >
            <ArrowUpwardRounded />
          </IconButton>
        </Box>
      </Paper>
    </>
  );
}
